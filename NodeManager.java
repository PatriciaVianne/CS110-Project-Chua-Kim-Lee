
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kim
 */
public class NodeManager {
    public NodeManager(){
        
    }
    //only works for beginning(when new nodes r made down
    //all new nodes have at least 2 keys
    //num 1 = less
    //num2 = greater
    public void changeNode(long location,RandomAccessFile db, long parent, long num1, long num2, long off1, long off2)throws IOException{
        db.seek(location);
        db.writeLong(parent);
        for(int i=0;i<11;i++){
            switch (i) {
                case 1: //write 1st child and offset in proper place
                    db.writeLong(num1); //write the first num in slot 1
                    db.writeLong(off1); //write its parent
                    break;
                case 4: //write 2nd child and offset in proper place
                    db.writeLong(num2); //write 2nd num in slot 2
                    db.writeLong(off2);
                    break;
                default:
                    db.writeLong(-1);
                    break;
            }
        }
        
    }
    public void addNode(RandomAccessFile db,long parent,long numNodes,long num1,long num2,long off1,long off2)throws IOException{
        long lastRecord = 112*numNodes+16;
        db.seek(lastRecord); //go to the end of the latest record (EOF)
        db.writeLong(parent); //write the parent
        for(int i=0;i<11;i++){
            switch (i) {
                case 1: //write 1st child and offset in proper place
                    db.writeLong(num1); //write the first num in slot 1
                    db.writeLong(off1); //write its parent
                    break;
                case 4: //write 2nd child and offset in proper place
                    db.writeLong(num2); //write 2nd num in slot 2
                    db.writeLong(off2);
                    break;
                default:
                    db.writeLong(-1);
                    break;
            }
        }
        numNodes++;
        db.seek(0);
        db.writeLong(numNodes); //update the numnodes at header
    }
    //turn the split shit into -1
    //offset = offset of currently inserted value (in case it is the mid)
    //NOTE CHECK IF A PARENT ALR EXISTS
    public void split(RandomAccessFile db,long numNodes,long[] arr,long parent,long[] offsets)throws IOException{
        long location = 112*parent+16+8; //skip parent
        long p = 112*parent+16; //check parent of current node being checked
        long[] arr2 = arr.clone(); //contains all keys (sorted cept for fifth
        //offsets is array of all offsets (ex: offsets[1] is the offset of arr2[1])
        long[] offsets2 = new long[5];
        Arrays.sort(arr); //arr is now sorted (including 5th)
        for(int i=0;i<5;i++){
            long hold = arr[i]; //check the sorted array
            for(int j=0;j<5;j++){
                if(arr2[j]==arr[i]){// 5 4 1 => 1 4 5
                    offsets2[i] = offsets[j]; //sort the offsets to pair up wif the ordered keys
                }
            }
        }
        //WHAT HAPPENS AFTER THE FOR LOOP??? HERE"S THE ANSWER
        //each element in arr is sorted alr rite?
        //so now theres this other array for offsets called offsets2
        //it pairs up each key wif its offset
        //ex: the offset of arr[1] is now in offsets2[1]
        //WHY DO THIS????
        //well, after sorting arr, all its keys are now messed up
        //that's where the clone comes in
        //before even sorting arr, we make a clone of it THAT IS STILL PAIRED WIF ITS OFFSETS
        //so we can use that (^^^^) pairing to be able to fix the keys' order
        //WITH REGARDS to the sorted array
        //NOTE TO SELF: make 1 new array (top) (handleparent) and another (right)
        //just use current for left
        long mid = arr[2];
        long midoff = offsets2[2];
//        db.seek(location);
        long id = handleParent(p,db,mid,midoff,numNodes);
        //addNode(db,mid,numNodes,arr[0],arr[1],offsets2[0],offsets2[1]); //id = numnodes-2 (left node)
        addNode(db,id,numNodes,arr[3],arr[4],offsets2[3],offsets2[4]); //id = numnodes-1 (right node)
        changeNode(location-8,db,id,arr[0],arr[1],offsets2[0],offsets2[1]);
//        db.writeLong(numNodes-2);
//        db.writeLong(mid);
//        db.writeLong(midoff);//offset of mid
//        db.writeLong(numNodes-1);
    }
    //id = id of parent
    public long handleParent(long id,RandomAccessFile db,long key,long offset,long numNodes)throws IOException{
        long pid=0; //id of where itll be placed
        if(id==-1){
            //if it has no parent to be pushed to, make a new node and set it as root
            addNode(db,-1,numNodes,key,-1,offset,-1);
            pid = numNodes-1;
            setRootNode(pid,db);
            //randomaccessfile,parent id,numnodes,key1,key2,offset1,offset2
        }
        else{
            //if it has a parent alr, and has space, write it there (shld prolly add a sort here)
            //prolly just call the sort function in btmanager after every split
            if(checkNotFull(id,db)){ //check the record based on id
                db.writeLong(numNodes-2);
                db.writeLong(key);
                db.writeLong(offset);
                db.writeLong(numNodes-1);
                pid = id;
            }
            else{ //if the one to be pushed to it is the 5th num
                long[] arr = new long[5]; //arr of keys
                long[] offsets = new long[5]; //arr of offsets
                long recid = 112*id+16;
                db.seek(recid);
                long par = db.readLong();//parent of record
                for(int i=0;i<4;i++){
                    recid+=8; //location of keys
                    db.seek(recid);
                    arr[i] = db.readLong(); //place key
                    offsets[i] = db.readLong(); //place respective offset
                }
                split(db,numNodes,arr,par,offsets);
                //^^^ WHAT IF IT SPLITS AND THE NUMBER RN IS THE NEW PARENT
            }
        }
        return pid;
    }
    //return true if node not full
    public boolean checkNotFull(long id,RandomAccessFile db) throws IOException{
        long recid = 112*id+16+8; //finding the record first(skip the parent also)
        for(int i=0;i<4;i++){
            recid+=16; //location of offset
            db.seek(recid);
            long offset = db.readLong();
            if(offset==-1){
                db.seek(recid-16); //go to the child node before the key thts empty
                return true;
            }
        }
        return false;
    }
    public long checkParentId(RandomAccessFile db,long id)throws IOException{
        long location = 112*id+16;
        db.seek(id);
        return db.readLong();
    }
    public void setRootNode(long id,RandomAccessFile db)throws IOException{
        db.seek(8);
        db.writeLong(id);
    }
}
