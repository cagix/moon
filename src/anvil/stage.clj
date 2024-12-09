(ns anvil.stage
  (:refer-clojure :exclude [get])
  (:require [anvil.app :as app]
            [anvil.graphics :as g]
            [anvil.ui :as ui]
            [clojure.utils :refer [pretty-pst with-err-str bind-root]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn get []
  (:stage ((app/current-screen) 1)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (g/gui-mouse-position)]
    (.hit (get) x y true)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (get))))
  (add-actor
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (Actor/.remove (::modal (get)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ g/gui-viewport-width 2)
                                 (* g/gui-viewport-height (/ 3 4))]
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

(def player-message-duration-seconds 1.5)

(def message-to-player nil)

(defn show-player-msg [message]
  (bind-root message-to-player {:message message :counter 0}))
