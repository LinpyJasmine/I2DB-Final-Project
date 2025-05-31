/*******************************************************************************
 * Copyright 2016, 2018 vanilladb.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.vanilladb.core.storage.index.IVFFlat;

import java.util.ArrayList;
import java.util.List;

import org.vanilladb.core.sql.VectorConstant;
import org.vanilladb.core.sql.distfn.DistanceFn;
import org.vanilladb.core.storage.index.Index;
import org.vanilladb.core.storage.index.SearchKey;
import org.vanilladb.core.storage.index.SearchKeyType;
import org.vanilladb.core.storage.index.SearchRange;
import org.vanilladb.core.storage.metadata.index.IndexInfo;
import org.vanilladb.core.storage.record.RecordId;
import org.vanilladb.core.storage.tx.Transaction;


public class IVFFlatIndex extends Index {
	
	
	public static final int CLUSTER_NUM = 100;

	public static final int TOP_K = 20;
	private List<IVFCluster> clusters;
	public int current_cluster_num = 0;
	private VectorConstant target_vector;

	public List<RecordId> top_k_vector_rids;
	public int current_return_idx = 0;


	

	/**
	 * Opens a hash index for the specified index.
	 * 
	 * @param ii
	 *            the information of this index
	 * @param keyType
	 *            the type of the search key
	 * @param tx
	 *            the calling transaction
	 */
	public IVFFlatIndex(IndexInfo ii, SearchKeyType keyType, Transaction tx) {
		super(ii, keyType, tx);

		this.clusters = new ArrayList<>(CLUSTER_NUM);
		for (int i = 0; i < CLUSTER_NUM; i++) {
			clusters.add(new IVFCluster());
		}
	}





	// Following is the override methods of Index interface
	@Override
	public void preLoadToMemory() {
		// DO NOTHEING AS THE DATABASE IS NOT ALLOWED TO PUT ALL DATAS IN MEMORY
		return;
	}

	
	@Override
	public void beforeFirst(SearchRange searchRange) {
		close();
		this.current_return_idx = 0;
		top_k_vector_rids.clear();
	}


	/**
	 * Moves to the next index record having the search key.
	 * 
	 * @see Index#next()
	 */
	@Override
	public boolean next() {
		if (current_return_idx >= CLUSTER_NUM)
			return false;

		return true;
	}

	/**
	 * Retrieves the data record ID from the current index record.
	 * 
	 * @see Index#getDataRecordId()
	 */
	@Override
	public RecordId getDataRecordId() {


		if (current_return_idx >= top_k_vector_rids.size() || top_k_vector_rids.get(current_return_idx) == null)
			return null;

		RecordId rid = top_k_vector_rids.get(current_return_idx);
		current_return_idx++;
		return rid;
	}

	/**
	 * Inserts a new index record into this index.
	 * 
	 * @see Index#insert(SearchKey, RecordId, boolean)
	 */
	@Override
	public void insert(SearchKey key, RecordId dataRecordId, boolean doLogicalLogging) {
		// insert the and do Kmeans
		target_vector = (VectorConstant)key.get(0);
		if (target_vector == null) {
			// If the target vector is null, we cannot insert it
			return;
		}
		if (current_cluster_num < CLUSTER_NUM) {
			for(int i = 0; i< CLUSTER_NUM; i++){
				if(clusters.get(i).getVectorCount()== 0){
					clusters.get(i).addVector(target_vector, dataRecordId);
					clusters.get(i).updateCentroid();
					current_cluster_num++;
					return;
				}
			}
		}

		double minDistance = Double.MAX_VALUE;
		int closestClusterIdx = -1;
		for (int i = 0; i < CLUSTER_NUM; i++) {
			double distance = EuclideanDistance(target_vector, clusters.get(i).getCentroid());
			if (distance < minDistance) {
				minDistance = distance;
				closestClusterIdx = i;
			}
		}

		// Add vector to closest cluster and update centroid
		IVFCluster closestCluster = clusters.get(closestClusterIdx);
		closestCluster.addVector(target_vector, dataRecordId);
		closestCluster.updateCentroid();
		

		// Optional: maximum of cluster



		return;
	}

	/**
	 * Deletes the specified index record.
	 * 
	 * @see Index#delete(SearchKey, RecordId, boolean)
	 */
	@Override
	public void delete(SearchKey key, RecordId dataRecordId, boolean doLogicalLogging) {
		// search in all clusters and delete the record if found
		target_vector = (VectorConstant)key.get(0);

		for (int i = 0; i < current_cluster_num; i++) {
			boolean deleted = clusters.get(i).delete(target_vector);
			if (deleted) {
				
				if(clusters.get(i).getVectorCount() == 0)
					current_cluster_num--;

				// If the vector is deleted, we can update the centroid
				clusters.get(i).updateCentroid();
				return;
			}
		}
		return;
	}

	/**
	 * Closes the index by closing the current table scan.
	 * 
	 * @see Index#close()
	 */
	@Override
	public void close() {
		//TODO
		return;
	}

	public void kmeans(DistanceFn embField){
		
		if (embField == null) {
			return;
		}
		int[] top_k_list= new int[this.TOP_K];

		// Step 1:
		if(current_cluster_num <= CLUSTER_NUM){
			// Create a new cluster
			IVFCluster newCluster = new IVFCluster(current_cluster_num, embField);
			this.clusters.add(newCluster);
			this.current_cluster_num++;
		}
		
		// Step 2: count the distance
		double[] distanceList=new double[CLUSTER_NUM];

		for (int i = 0; i < CLUSTER_NUM; i++) {
			if (clusters.get(i).getVectorCount() == 0) {
				// If the cluster is empty, we can skip it
				distanceList[i] = -1;
				continue;
			}
			double distance = embField.distance(clusters.get(i).getCentroid());
			distanceList[i] = distance;
		}

		// Step 3: sort the distance and get the top K clusters idx and set the record rid 
		int[] topKIndices = new int[TOP_K];
		double[] topKDistances = new double[TOP_K];
		for (int i = 0; i < TOP_K; i++) {
			topKIndices[i] = -1;
			topKDistances[i] = Double.MAX_VALUE;
		}
		for (int i = 0; i < CLUSTER_NUM; i++) {
			double dist = distanceList[i];
			if(dist == -1){
				continue; // Skip empty clusters
			}
			for (int j = 0; j < TOP_K; j++) {
				if (dist < topKDistances[j]) {
					// shift existing
					for (int k = TOP_K - 1; k > j; k--) {
						topKDistances[k] = topKDistances[k - 1];
						topKIndices[k] = topKIndices[k - 1];
					}
					// insert
					topKDistances[j] = dist;
					topKIndices[j] = i;
					break;
				}
			}
		}

		//Step 4: set the top K rid
		top_k_vector_rids.clear(); // clear existing entries

		for (int idx : topKIndices) {
			if (idx == -1) continue;
			List<RecordId> rids = clusters.get(idx).getRecordIds();
			for (int i = 0; i < rids.size(); i++) {
				top_k_vector_rids.add(rids.get(i));
			}
		}





		return;
	}
	// Helper function for caaulating Euclidean distance
	private double EuclideanDistance(VectorConstant vec1, VectorConstant vec2) {
        double sum = 0;
		if(vec1.dimension() != vec2.dimension()) {
			throw new IllegalArgumentException("Vectors must have the same dimension");
		}

        for (int i = 0; i < vec1.dimension(); i++) {
            double diff = vec1.get(i) - vec2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

	
}
