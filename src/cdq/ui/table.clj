(ns cdq.ui.table)

(defprotocol Table
  (add! [_ actor-or-decl])
  (cells [_])
  (add-rows! [_ rows])
  (set-opts! [_ opts]))
