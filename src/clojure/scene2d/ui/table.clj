(ns clojure.scene2d.ui.table)

(defprotocol Table
  (add! [_ actor-or-decl])
  (cells [_])
  (add-rows! [_ rows])
  (set-opts! [_ opts]))
