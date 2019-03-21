(ns konstellate.resource-desc)

(defmacro defdesc
  [kind bind & body]
  `(defmethod describe ~(str (name kind))
     ~bind
     (merge (konstellate.resource-desc/general ~(first bind))
            ~@body)))

