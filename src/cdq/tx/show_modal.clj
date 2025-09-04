(ns cdq.tx.show-modal
  (:require [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.gdx.ui :as ui]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn- show-modal-window! [stage
                           ui-viewport
                           {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (ui/window {:title title
                          :rows [[{:actor {:actor/type :actor.type/label
                                           :label/text text}}]
                                 [(ui/text-button button-text
                                                  (fn [_actor _ctx]
                                                    (actor/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:viewport/width  ui-viewport) 2)
                                            (* (:viewport/height ui-viewport) (/ 3 4))]
                          :pack? true})))

(defn do! [[_ opts] {:keys [ctx/stage
                            ctx/ui-viewport]}]
  (show-modal-window! stage
                      ui-viewport
                      opts)
  nil)
