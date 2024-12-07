(ns forge.ui
  (:require [forge.app.gui-viewport :refer [gui-viewport-width
                                            gui-viewport-height]]
            [forge.app.vis-ui :as ui]
            [forge.graphics :as g]
            [forge.screens.stage :refer [screen-stage add-actor]]
            [forge.utils :refer [pretty-pst
                                 with-err-str]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn background-image []
  (ui/image->widget (g/->image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (screen-stage))))
  (add-actor
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (Actor/.remove (::modal (screen-stage)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ gui-viewport-width 2)
                                 (* gui-viewport-height (/ 3 4))]
               :pack? true})))



(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))
