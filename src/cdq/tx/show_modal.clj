(ns cdq.tx.show-modal
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [clojure.gdx.ui :as ui]
            [cdq.ui.stage :as stage]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn- show-modal! [{:keys [ctx/ui-viewport
                            ctx/stage]}
                    {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (ui/window {:title title
                          :rows [[(ui/label text)]
                                 [(ui/text-button button-text
                                                  (fn [_actor _ctx]
                                                    (ui/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width ui-viewport) 2)
                                            (* (:height ui-viewport) (/ 3 4))]
                          :pack? true})))

(defmethod do! :tx/show-modal [[_ opts] ctx]
  (show-modal! ctx opts))
