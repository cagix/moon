(ns clojure.scene2d.ui.table)

(defprotocol Table
  (add! [table actor-or-decl])
  (cells [_])
  (add-rows! [_ rows]
             "rows is a seq of seqs of columns.
             Elements are actors or nil (for just adding empty cells ) or a map of
             {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required.")
  (set-opts! [_ opts]))
