package kr.shacon.edi;

interface Evaluator<T, S> {

  void eval(T target, S source);

}
