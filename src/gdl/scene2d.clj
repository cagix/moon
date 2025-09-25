(ns gdl.scene2d)

(defmulti build
  "Builds the `gdl.scene2d.actor` via the supplied options map.

  Dispatches on `:actor/type`."
  :actor/type)
