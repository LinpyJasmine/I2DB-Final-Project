package org.vanilladb.core.storage.index.IVFFlat;

import java.util.ArrayList;
import java.util.List;

import org.vanilladb.core.sql.VectorConstant;
import org.vanilladb.core.sql.distfn.DistanceFn;
import org.vanilladb.core.storage.record.RecordId;

public class IVFCluster {
    private int clusterIdx;                 // cluster index
	private DistanceFn embField;
    private VectorConstant centroid;               // centroid vector
    private List<VectorConstant> vectorList;       // list of vectors in the cluster
	private List<RecordId> recordIdList = new ArrayList<>();
	private int vectorCount;             // number of vectors in the cluster

	public IVFCluster() {
        this.vectorList = new ArrayList<>();
		this.vectorCount = 0;
    }
    public IVFCluster(int clusterIdx, DistanceFn embField ) {
        this.clusterIdx = clusterIdx;
		this.embField = embField;
        this.centroid = embField.query;
        this.vectorList = new ArrayList<>();
		this.vectorCount = 0;
    }

    public int getClusterIdx() {
        return clusterIdx;
    }

    public VectorConstant getCentroid() {
        return centroid;
    }

    public List<VectorConstant> getVectorList() {
        return vectorList;
    }

	public int getVectorCount() {
		return vectorCount;
	}

	public List<RecordId> getRecordIds() {
        return recordIdList;
    }

    public void addVector(VectorConstant vector, RecordId rid) {
        vectorList.add(vector);
        recordIdList.add(rid);
    }

	public boolean delete(VectorConstant vector) {
        int idx = vectorList.indexOf(vector);
        if (idx >= 0) {
            vectorList.remove(idx);
            recordIdList.remove(idx);
            return true;
        }
        return false;
    }

	public boolean checkExistence(VectorConstant vector) {
		return vectorList.contains(vector);
	}




	public boolean updateCentroid() {
		if (vectorList.isEmpty()) {
			return false; // no vectors, nothing to update
		}

		int dim = centroid.dimension();
		float[] newCentroidValues = new float[dim];

		// Sum all vectors
		for (VectorConstant vec : vectorList) {
			for (int i = 0; i < dim; i++) {
				newCentroidValues[i] += vec.get(i);
			}
		}

		// Average
		for (int i = 0; i < dim; i++) {
			newCentroidValues[i] /= vectorList.size();
		}

		// Check if centroid updated (using Euclidean distance)
		VectorConstant newCentroid = new VectorConstant(newCentroidValues);
		double shift = EuclideanDistance(centroid, newCentroid);
		boolean isUpdated = shift > 1e-6; // small threshold
		if (isUpdated) {
			centroid = newCentroid;
		}

		return isUpdated;
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
