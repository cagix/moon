(ns cdq.ui.modal
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn create [{:keys [title text button-text on-click]}]
  (assert (not (::modal ctx/stage)))
  (ui/window {:title title
              :rows [[(ui/label text)]
                     [(ui/text-button button-text
                                      (fn []
                                        (Actor/.remove (::modal ctx/stage))
                                        (on-click)))]]
              :id ::modal
              :modal? true
              :center-position [(/ (:width  ctx/ui-viewport) 2)
                                (* (:height ctx/ui-viewport) (/ 3 4))]
              :pack? true}))
