(ns cdq.ui.stage
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.utils :refer [pretty-pst with-err-str]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.scenes.scene2d.ui ButtonGroup)))

(defn mouse-on-actor? []
  (let [[x y] (graphics/mouse-position #_(Stage/.getViewport ui/stage))]
    (Stage/.hit ui/stage x y true)))

(defn add-actor [actor]
  (Stage/.addActor ui/stage actor))

(defn get-inventory []
  (get (:windows ui/stage) :inventory-window))

(defn get-action-bar []
  (let [group (:ui/action-bar (:action-bar-table ui/stage))]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (Group/.findActor group "action-bar/button-group"))}))

(defn selected-skill []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (Actor/.getUserObject skill-button)))

(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor (ui/window {:title "Error"
                         :rows [[(ui/label (binding [*print-level* 3]
                                             (with-err-str
                                               (clojure.repl/pst throwable))))]]
                         :modal? true
                         :close-button? true
                         :close-on-escape? true
                         :center? true
                         :pack? true})))
