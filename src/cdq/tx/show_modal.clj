(ns cdq.tx.show-modal
  (:require [gdl.ui :as ui]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn do! [{:keys [ctx/stage
                   ctx/ui-viewport]}
           {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (ui/add! stage
           (ui/window {:title title
                       :rows [[(ui/label text)]
                              [(ui/text-button button-text
                                               (fn [_actor _ctx]
                                                 (ui/remove! (::modal stage))
                                                 (on-click)))]]
                       :id ::modal
                       :modal? true
                       :center-position [(/ (:width  ui-viewport) 2)
                                         (* (:height ui-viewport) (/ 3 4))]
                       :pack? true})))
