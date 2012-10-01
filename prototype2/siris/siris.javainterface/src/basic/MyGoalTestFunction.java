package basic;

import siris.pacman.graph.Node;

public class MyGoalTestFunction implements siris.pacman.graph.GoalTestFunction {

	private String goal = "basic.MyPacman" ;

	public MyGoalTestFunction() {
	}
	
	@Override
	public boolean testGoal(Node n) {
		if (n.getClass().getName().equals(goal))
			return true;
		return false ;
	}
	
	
	
// ------------ only for Testing --------------------
	

//	public JavaInterface ji;
//	
//	public MyGoalTestFunction(JavaInterface javaInt){
//		ji = javaInt;
//	}
//	
//	@Override
//	public boolean testGoal(Node n) {
//		ji.swap(n.id());
//		try {
//			Thread.sleep(400);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		return false ;
//	}

}
