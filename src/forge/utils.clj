(ns forge.utils)

(defmacro bind-root [sym value]
  `(clojure.lang.Var/.bindRoot (var ~sym) ~value))
