(ns forge.stage
  (:require [clojure.gdx.scene2d.stage :as stage]
            [forge.app :as app]
            [forge.graphics :refer [gui-mouse-position
                                    gui-viewport-width
                                    gui-viewport-height]]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]))

(defn mouse-on-actor? []
  (stage/hit (app/stage) (gui-mouse-position) :touchable? true))

(defn add-actor [actor]
  (stage/add (app/stage) actor))

; TODO the below code is an addition, not interependent with above logic

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (app/stage))))
  (add-actor
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (actor/remove! (::modal (app/stage)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ gui-viewport-width 2)
                                 (* gui-viewport-height (/ 3 4))]
               :pack? true})))
