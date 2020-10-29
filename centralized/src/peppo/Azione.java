package peppo;

import logist.task.Task;

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


	Type getType() {
		return type;
	}

}
