(ns cdq.ui
  (:require [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.message :as message]
            [clojure.gdx.viewport :as viewport]
            [clojure.scene2d :as scene2d])
  (:import (cdq.ui Stage)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn- add-actors! [^Stage stage actor-fns ctx]
  (doseq [[actor-fn & params] actor-fns]
    (.addActor stage (scene2d/build (apply actor-fn ctx params)))))

(defn create
  [{:keys [ctx/graphics]
    :as ctx}
   actor-fns]
  (let [stage (Stage. (:graphics/ui-viewport graphics)
                      (:graphics/batch       graphics))
        actor-fns (map #(update % 0 requiring-resolve) actor-fns)
        ctx (assoc ctx
                   :ctx/stage stage
                   :ctx/actor-fns actor-fns)]
    (add-actors! stage actor-fns ctx)
    ctx))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn- stage-find [^Stage stage k]
  (-> stage
      .getRoot
      (.findActor k)))

(defn show-data-viewer! [this data]
  (.addActor this (scene2d/build
                   {:actor/type :actor.type/data-viewer
                    :title "Data View"
                    :data data
                    :width 500
                    :height 500})))

(defn viewport-width  [stage] (viewport/world-width  (.getViewport stage)))
(defn viewport-height [stage] (viewport/world-height (.getViewport stage)))

(defn get-ctx [this]
  (.ctx this))

(defn mouseover-actor [this position]
  (let [[x y] (viewport/unproject (.getViewport this) position)]
    (.hit this x y true)))

(defn action-bar-selected-skill [this]
  (-> this
      .getRoot
      (.findActor "cdq.ui.action-bar")
      action-bar/selected-skill))

(defn rebuild-actors! [stage ctx]
  (.clear stage)
  (add-actors! stage (:ctx/actor-fns ctx) ctx))

(defn inventory-window-visible? [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (Group/.findActor "cdq.ui.windows.inventory")
      Actor/.isVisible))

(defn toggle-inventory-visible! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (Group/.findActor "cdq.ui.windows.inventory")
      toggle-visible!))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal-window! [stage ui-viewport {:keys [title text button-text on-click]}]
  (assert (not (-> stage
                   .getRoot
                   (Group/.findActor "cdq.ui.modal-window"))))
  (.addActor stage
             (scene2d/build
              {:actor/type :actor.type/window
               :title title
               :rows [[{:actor {:actor/type :actor.type/label
                                :label/text text}}]
                      [{:actor {:actor/type :actor.type/text-button
                                :text button-text
                                :on-clicked (fn [_actor _ctx]
                                              (.remove (-> stage
                                                           .getRoot
                                                           (.findActor "cdq.ui.modal-window")))
                                              (on-click))}}]]
               :actor/name "cdq.ui.modal-window"
               :modal? true
               :actor/center-position [(/ (viewport/world-width  ui-viewport) 2)
                                       (* (viewport/world-height ui-viewport) (/ 3 4))]
               :pack? true})))

(defn set-item! [stage cell item-properties]
  (-> stage
      (stage-find "cdq.ui.windows")
      (Group/.findActor "cdq.ui.windows.inventory")
      (inventory-window/set-item! cell item-properties)))

(defn remove-item! [stage inventory-cell]
  (-> stage
      (stage-find "cdq.ui.windows")
      (Group/.findActor "cdq.ui.windows.inventory")
      (inventory-window/remove-item! inventory-cell)))

(defn add-skill! [stage skill-properties]
  (-> stage
      .getRoot
      (Group/.findActor "cdq.ui.action-bar")
      (action-bar/add-skill! skill-properties)))

(defn remove-skill! [stage skill-id]
  (-> stage
      .getRoot
      (Group/.findActor "cdq.ui.action-bar")
      (action-bar/remove-skill! skill-id)))

(defn show-text-message! [stage message]
  (-> stage
      .getRoot
      (Group/.findActor "player-message")
      (message/show! message)))

(defn toggle-entity-info-window! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (Group/.findActor "cdq.ui.windows.entity-info")
      toggle-visible!))

(defn close-all-windows! [stage]
  (->> (stage-find stage "cdq.ui.windows")
       Group/.getChildren
       (run! #(Actor/.setVisible % false))))

(defprotocol ActorInformation
  (actor-information [_ actor]))

(defprotocol ErrorWindow
  (show-error-window! [_ throwable]))
