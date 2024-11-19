(ns moon.widgets.modal
  (:require [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.core :refer [gui-viewport-width gui-viewport-height]]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show [{:keys [title text button-text on-click]}]
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
               :center-position [(/ (gui-viewport-width) 2)
                                 (* (gui-viewport-height) (/ 3 4))]
               :pack? true})))
