(ns clojure.scene2d.ui.table)

(defprotocol Table
  (add! [table actor-or-decl])
  (cells [_])
  (add-rows! [_ rows])
  (set-opts! [_ opts]))
