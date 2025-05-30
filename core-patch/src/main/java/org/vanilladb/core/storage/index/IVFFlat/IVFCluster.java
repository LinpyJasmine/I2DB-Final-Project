package org.vanilladb.core.storage.index.IVFFlat;

import java.util.ArrayList;
import java.util.List;

import org.vanilladb.core.sql.VectorConstant;

public class IVFCluster {
    private int clusterIdx;                 // cluster index
    private float[] centroid;               // centroid vector
    private List<float[]> vectorList;       // list of vectors in the cluster
	private int vectorCount;             // number of vectors in the cluster

    public IVFCluster(int clusterIdx, float[] centroid) {
        this.clusterIdx = clusterIdx;
        this.centroid = centroid;
        this.vectorList = new ArrayList<>();
		this.vectorCount = 0;
    }

    public int getClusterIdx() {
        return clusterIdx;
    }

    public float[] getCentroid() {
        return centroid;
    }

    public List<float[]> getVectorList() {
        return vectorList;
    }

	public int getVectorCount() {
		return vectorCount;
	}

    public void addVector(float[] vector) {
        vectorList.add(vector);
		vectorCount++;
    }

	public void delete(float[] vector) {
		vectorList.remove(vector);
		vectorCount--;
    }

	public boolean checkExistence(float[] vector) {
		return vectorList.contains(vector);
	}




	public boolean updateCentroid() {
		if (vectorList.isEmpty()) {
			return false; // no vectors, nothing to update
		}

		int dim = centroid.length;
		float[] newCentroid = new float[dim];

		// Sum all vectors
		for (float[] vec : vectorList) {
			for (int i = 0; i < dim; i++) {
				newCentroid[i] += vec[i];
			}
		}

		// Average
		for (int i = 0; i < dim; i++) {
			newCentroid[i] /= vectorList.size();
		}

		// Check if centroid updated (using Euclidean distance)
		VectorConstant oldCentroidVec = new VectorConstant(centroid);
		VectorConstant newCentroidVec = new VectorConstant(newCentroid);
		double shift = EuclideanDistance(oldCentroidVec, newCentroidVec);
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
