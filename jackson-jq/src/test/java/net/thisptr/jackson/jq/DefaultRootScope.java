package net.thisptr.jackson.jq;

public class DefaultRootScope {
	private static final Scope ROOT_SCOPE = Scope.newEmptyScope();
	static {
		ROOT_SCOPE.loadFunctions(Scope.class.getClassLoader());
	}

	public static Scope getInstance() {
		return ROOT_SCOPE;
	}
}
