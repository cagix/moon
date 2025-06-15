(ns gdl.ui.table)

(defprotocol Table
  (add-rows! [_ rows]
             "rows is a seq of seqs of columns.
             Elements are actors or nil (for just adding empty cells ) or a map of
             {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."))
