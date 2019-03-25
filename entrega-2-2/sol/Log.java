/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipes_service.tsae.data_structures;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


import recipes_service.data.Operation;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class Log implements Serializable{

	private static final long serialVersionUID = -4864990265268259700L;
	/**
	 * This class implements a log, that stores the operations
	 * received  by a client.
	 * They are stored in a ConcurrentHashMap (a hash table),
	 * that stores a list of operations for each member of 
	 * the group.
	 */
	private ConcurrentHashMap<String, List<Operation>> log= new ConcurrentHashMap<String, List<Operation>>();  

	public Log(List<String> participants){
		// create an empty log
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			log.put(it.next(), new Vector<Operation>());
		}
	}

	/**
	 * inserts an operation into the log. Operations are 
	 * inserted in order. If the last operation for 
	 * the user is not the previous operation than the one 
	 * being inserted, the insertion will fail.
	 * 
	 * @param op
	 * @return true if op is inserted, false otherwise.
	 */
	public synchronized boolean add(Operation op){
		
		Timestamp timestamp = op.getTimestamp();
		String hostId = timestamp.getHostid();
		       
		Timestamp lastTimestamp;
				
        List<Operation> operations = this.log.get(hostId);

        if (operations == null || operations.isEmpty()) {
        	lastTimestamp = null;
        } else {
        	lastTimestamp = operations.get(operations.size() - 1).getTimestamp();
        }
         
        long timestampDifference = op.getTimestamp().compare(lastTimestamp);
        
        if ((lastTimestamp == null && timestampDifference == 0) || (lastTimestamp != null && timestampDifference == 1)) {
            this.log.get(hostId).add(op);
            return true;
        } else {
            return false;
        }
	}
	
	/**
	 * Checks the received summary (sum) and determines the operations
	 * contained in the log that have not been seen by
	 * the proprietary of the summary.
	 * Returns them in an ordered list.
	 * @param sum
	 * @return list of operations
	 */
	public synchronized List<Operation> listNewer(TimestampVector sum){
		
	       List<Operation> missingList = new Vector<Operation>();



	        for (String node : this.log.keySet()) {
	            List<Operation> operations = this.log.get(node);
	            Timestamp timestampToCompare = sum.getLast(node);


	            for (Operation op : operations) {
	                if (op.getTimestamp().compare(timestampToCompare) > 0) {
	                    missingList.add(op);
	                }
	            }
	        }
	        return missingList;
	        
	}
	
	/**
	 * Removes from the log the operations that have
	 * been acknowledged by all the members
	 * of the group, according to the provided
	 * ackSummary. 
	 * @param ack: ackSummary.
	 */
	public synchronized void purgeLog(TimestampMatrix ack){
		
		 TimestampVector minTimestampVector = ack.minTimestampVector();
		 
		 for (Entry<String, List<Operation>> entry : log.entrySet()) {
	            String participant = entry.getKey();
	            List<Operation> operations = entry.getValue();
	            Timestamp lastTimestamp = minTimestampVector.getLast(participant);
	            
	            if (lastTimestamp == null) {
	                continue;
	            }
	            
	            for (int i = operations.size() - 1; i >= 0; i--) {
	                Operation op = operations.get(i);

	                if (op.getTimestamp().compare(lastTimestamp) < 0) {
	                    operations.remove(i);
	                }
	            }
	        }
		
	}

	/**
	 * equals
	 */
	@Override
	public synchronized boolean equals(Object obj) {
		
//		System.out.println("Inicio equals log");
        
		if (obj == null) {
            return false;
 //       } else if (this == obj) {
 //           return true;
        } else if (!(obj instanceof Log)) {
            return false;
        }

        Log other = (Log) obj;
        
        if (!other.log.equals(this.log)) {
        	return false;

        } else {
            return this.log.equals(other.log);
        }        
	}

	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String name="";
		for(Enumeration<List<Operation>> en=log.elements();
		en.hasMoreElements(); ){
		List<Operation> sublog=en.nextElement();
		for(ListIterator<Operation> en2=sublog.listIterator(); en2.hasNext();){
			name+=en2.next().toString()+"\n";
		}
	}
		
		return name;
	}
}
