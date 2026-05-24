package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        if(matrix == null){
            throw new IllegalArgumentException("[SharedMatrix]: Matrix cannot be null");
        }
        vectors=new SharedVector[matrix.length];
        for(int i=0; i < matrix.length; i++){
            vectors[i]=new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        if(matrix == null){
            throw new IllegalArgumentException("[SharedMatrix]: Matrix cannot be null");
        }
        SharedVector[] copy=new SharedVector[matrix.length];
        for(int i=0; i < matrix.length; i++){
            copy[i]=new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
        vectors=copy;
    }

    public void loadColumnMajor(double[][] matrix) {
        if(matrix == null){
            throw new IllegalArgumentException("[SharedMatrix]: Matrix cannot be null");
        }
        if(matrix.length == 0){
            throw new IllegalArgumentException("[SharedMatrix]: Matrix cannot be empty");
        }
        int otherCol=matrix[0].length;
        SharedVector[] copy=new SharedVector[otherCol]; 
        for (int c = 0; c < otherCol; c++){
            double[] normalVector=new double[matrix.length];
            for(int r = 0; r < matrix.length;r++){
                normalVector[r]=matrix[r][c];
            }
            copy[c]=new SharedVector(normalVector, VectorOrientation.COLUMN_MAJOR);
        }  
        vectors=copy;
    }

    public double[][] readRowMajor() {
    // TODO: return matrix contents as a row-major double[][]
        acquireAllVectorReadLocks(vectors);
        try {
            double[][] output;
            if (this.length() == 0)
                output = new double[0][0];
            else {
                if (vectors[0].getOrientation() == VectorOrientation.ROW_MAJOR){ //Case: Row Major
                    output = new double[this.length()][vectors[0].length()];
                    for (int i = 0; i < this.length(); i++) {
                        for (int j = 0; j < vectors[0].length(); j++) {
                            output[i][j] = vectors[i].get(j);
                        }
                    }
                } 
                else {    //Case: Column Major
                    output = new double[vectors[0].length()][length()];
                    for (int i = 0; i < vectors[0].length(); i++) {
                        for (int j = 0; j < vectors.length; j++) {
                            output[i][j] = vectors[j].get(i);
                        }
                    }
                }
            }
            return output;
        } 
        finally {
            releaseAllVectorReadLocks(vectors);
        }
    }

    public SharedVector get(int index) {
        if(index < 0 || index >= vectors.length)
            throw new IndexOutOfBoundsException("[SharedVector get]: Index given is invalid");
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
        
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if(vectors.length==0)
            throw new IndexOutOfBoundsException("[getOrientation]: Given matrix is empty");
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for(int i=0;i<vecs.length;i++){
            vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for(int i=0;i<vecs.length;i++){
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for(int i=0;i<vecs.length;i++){
            vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write lock for each vector
        for(int i = 0; i < vecs.length; i++){
            vecs[i].writeUnlock();
        }
    }
}
