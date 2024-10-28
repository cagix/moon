(ns moon.widgets.player-modal
  (:require [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :as component]
            [moon.graphics.gui-view :as gui-view]
            [moon.stage :as stage]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn- show-player-modal! [{:keys [title text button-text on-click]}]
  (assert (not (::modal (stage/get))))
  (stage/add!
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (a/remove! (::modal (stage/get)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ (gui-view/width) 2)
                                 (* (gui-view/height) (/ 3 4))]
               :pack? true})))

(defc :tx/player-modal
  (component/handle [[_ params]]
    (show-player-modal! params)
    nil))
