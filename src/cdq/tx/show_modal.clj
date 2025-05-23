(ns cdq.tx.show-modal
  (:require [cdq.g :as g]
            [gdl.ui :as ui]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn do! [ctx
           {:keys [title text button-text on-click]}]
  (assert (not (g/get-actor ctx ::modal)))
  (g/add-actor! ctx
                (ui/window {:title title
                            :rows [[(ui/label text)]
                                   [(ui/text-button button-text
                                                    (fn [_actor ctx]
                                                      (ui/remove! (g/get-actor ctx ::modal))
                                                      (on-click)))]]
                            :id ::modal
                            :modal? true
                            :center-position [(/ (g/ui-viewport-width ctx) 2)
                                              (* (g/ui-viewport-height ctx) (/ 3 4))]
                            :pack? true})))
