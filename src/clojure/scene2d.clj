(ns clojure.scene2d)

(defmulti build
  "Builds the `clojure.scene2d.actor` via the supplied options map.

  Dispatches on `:actor/type`."
  :actor/type)
