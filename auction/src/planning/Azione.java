package planning;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

class Azione {
	enum Type {PICKUP, DELIVERY};
	
	private Task task;
	private Type type;
	
	
	
	Azione(Task task, Type type) {
		super();
		this.task = task;
		this.type = type;
	}


	Task getTask() {
		return task;
	}
	
	void setTask(Task newTask) {
		task = newTask;
	}


	Type getType() {
		return type;
	}
	
	
	/**
	 * @return the city where this action takes place
	 */
	City getCity() {
		if(type == Type.PICKUP) {
			return task.pickupCity;
		}
		return task.deliveryCity;
	}

	
	/**
	 * @return the Action corresponding to this Azione
	 */
	Action getAction() {
		if(type == Type.PICKUP) {
			return new Action.Pickup(task);
		}
		return new Action.Delivery(task);
	}
}
