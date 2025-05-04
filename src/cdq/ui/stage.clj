(ns cdq.ui.stage
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.utils :refer [pretty-pst with-err-str]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.scenes.scene2d.ui ButtonGroup)))

(defn mouse-on-actor? [stage]
  (let [[x y] (graphics/mouse-position #_(Stage/.getViewport stage))]
    (Stage/.hit stage x y true)))

(defn add-actor [stage actor]
  (Stage/.addActor stage actor))

(defn get-inventory [stage]
  (get (:windows stage) :inventory-window))

(defn get-action-bar [stage]
  (let [group (:ui/action-bar (:action-bar-table stage))]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (Group/.findActor group "action-bar/button-group"))}))

(defn selected-skill [stage]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar stage)))]
    (Actor/.getUserObject skill-button)))

(defn error-window! [stage throwable]
  (pretty-pst throwable)
  (add-actor stage
             (ui/window {:title "Error"
                         :rows [[(ui/label (binding [*print-level* 3]
                                             (with-err-str
                                               (clojure.repl/pst throwable))))]]
                         :modal? true
                         :close-button? true
                         :close-on-escape? true
                         :center? true
                         :pack? true})))
