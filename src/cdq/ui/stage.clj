(ns cdq.ui.stage
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.utils :refer [pretty-pst with-err-str]])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.scenes.scene2d.ui ButtonGroup)))

(declare ^:private ^Stage stage)

(defn init! []
  (let [stage (proxy [Stage ILookup] [graphics/ui-viewport graphics/batch]
                (valAt
                  ([id]
                   (ui/find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (ui/find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (.setInputProcessor Gdx/input stage)
    (.bindRoot #'stage stage)))

(defn clear! []
  (.clear stage))

(defn draw! []
  (.draw stage))

(defn act! []
  (.act stage))

(defn get-actor [id-keyword]
  (id-keyword stage))

(declare player-message)

(defn init-state! []
  (.bindRoot #'player-message (atom {:duration-seconds 1.5})))

(defn show-player-msg! [text]
  (swap! player-message assoc :text text :counter 0))

(defn mouse-on-actor? []
  (let [[x y] (graphics/mouse-position #_(Stage/.getViewport stage))]
    (Stage/.hit stage x y true)))

(defn add-actor [actor]
  (Stage/.addActor stage actor))

(defn get-inventory []
  (get (:windows stage) :inventory-window))

(defn get-action-bar []
  (let [group (:ui/action-bar (:action-bar-table stage))]
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
