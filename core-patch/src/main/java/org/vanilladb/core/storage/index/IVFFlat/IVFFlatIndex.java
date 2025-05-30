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

import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.storage.index.Index;
import org.vanilladb.core.storage.index.SearchKey;
import org.vanilladb.core.storage.index.SearchKeyType;
import org.vanilladb.core.storage.index.SearchRange;
import org.vanilladb.core.storage.metadata.index.IndexInfo;
import org.vanilladb.core.storage.record.RecordFile;
import org.vanilladb.core.storage.record.RecordId;
import org.vanilladb.core.storage.tx.Transaction;


public class IVFFlatIndex extends Index {
	
	
	public static final int CLUSTER_NUM = 100;

	public static final int TOP_K = 20;
	private List<IVFCluster> clusters;
	public int current_cluster_num = 0;
	private Constant target_vector;

	public RecordId[] top_k_vector_rid;
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
	}














	// Following is the override methods of Index interface
	@Override
	public void preLoadToMemory() {
		// DO NOTHEING AS THE DATABASE IS NOT ALLOWED TO PUT ALL DATAS IN MEMORY
		return;
	}

	/**
	 * Positions the index before the first index record having the specified
	 * search key. The method hashes the search key to determine the bucket, and
	 * then opens a {@link RecordFile} on the file corresponding to the bucket.
	 * The record file for the previous bucket (if any) is closed.
	 * 
	 * @see Index#beforeFirst(SearchRange)
	 */
	@Override
	public void beforeFirst(SearchRange searchRange) {
		close();
		this.current_return_idx = 0;
		for (int i = 0; i < top_k_vector_rid.length; i++) {
			top_k_vector_rid[i] = null;
		}
		// support the equality query only
		if (!searchRange.isSingleValue())
			throw new UnsupportedOperationException();

		SearchKey targetKey = searchRange.asSearchKey();

		
		this.target_vector = targetKey.get(0);
		

		kmeans(this.target_vector);
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
		RecordId rid = this.top_k_vector_rid[current_return_idx];
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
	
		return;
	}

	/**
	 * Deletes the specified index record.
	 * 
	 * @see Index#delete(SearchKey, RecordId, boolean)
	 */
	@Override
	public void delete(SearchKey key, RecordId dataRecordId, boolean doLogicalLogging) {
		//



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

	public void kmeans(Constant target_vector){

		// Step 1:
		if(current_cluster_num <= CLUSTER_NUM){
			// Create a new cluster
			IVFCluster newCluster = new IVFCluster(current_cluster_num, target_vector.getVector());
			this.clusters.add(newCluster);
			this.current_cluster_num++;
		}
		else {
			// Find the closest cluster to the target vector
			float minDistance = Float.MAX_VALUE;
			int closestClusterIdx = -1;

			for (int i = 0; i < current_cluster_num; i++) {
				float distance = DistanceCalculator.calculateDistance(target_vector, clusters.get(i).getCentroid());
				if (distance < minDistance) {
					minDistance = distance;
					closestClusterIdx = i;
				}
			}

			// Add the target vector to the closest cluster
			clusters.get(closestClusterIdx).addVector(target_vector.getVector());
			clusters.get(closestClusterIdx).updateCentroid();
		}
		// Count dis


		return;
	}
	
}
