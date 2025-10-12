(ns cdq.ui
  (:require cdq.ui.widget
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.stage :as stage]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.message :as message]
            [clojure.scene2d.vis-ui :as vis-ui]
            [clojure.scene2d.vis-ui.text-button :as text-button]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.gdx.math.vector2 :as vector2]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.vis-ui.label :as label])
  (:import (cdq.ui Stage)))

(defn- add-actors! [^Stage stage actor-fns ctx]
  (doseq [[actor-fn & params] actor-fns]
    (.addActor stage (apply actor-fn ctx params))))

(defn create!
  [{:keys [ctx/graphics]
    :as ctx}
   actor-fns]
  (vis-ui/load! {:skin-scale :x1})
  (let [stage (Stage. (:graphics/ui-viewport graphics)
                      (:graphics/batch       graphics))
        actor-fns (map #(update % 0 requiring-resolve) actor-fns)
        ctx (assoc ctx
                   :ctx/stage stage
                   :ctx/actor-fns actor-fns)]
    (add-actors! stage actor-fns ctx)
    ctx))

(defn dispose! []
  (vis-ui/dispose!))

(defn toggle-visible! [actor]
  (actor/set-visible! actor (not (actor/visible? actor))))

(defn- stage-find [^Stage stage k]
  (-> stage
      .getRoot
      (.findActor k)))

(defn show-data-viewer! [this data]
  (.addActor this (cdq.ui.widget/data-viewer
                   {:title "Data View"
                    :data data
                    :width 500
                    :height 500})))

(defn viewport-width  [stage] (viewport/world-width  (stage/viewport stage)))
(defn viewport-height [stage] (viewport/world-height (stage/viewport stage)))

(defn get-ctx [this]
  (.ctx this))

(defn mouseover-actor [this position]
  (let [[x y] (vector2/->clj (viewport/unproject (stage/viewport this) (vector2/->java position)))]
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
      (group/find-actor "cdq.ui.windows.inventory")
      actor/visible?))

(defn toggle-inventory-visible! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      toggle-visible!))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal-window! [stage ui-viewport {:keys [title text button-text on-click]}]
  (assert (not (-> stage
                   .getRoot
                   (group/find-actor "cdq.ui.modal-window"))))
  (.addActor stage
             (window/create
              {:title title
               :rows [[{:actor (label/create text)}]
                      [{:actor (text-button/create
                                {:text button-text
                                 :on-clicked (fn [_actor _ctx]
                                               (.remove (-> stage
                                                            .getRoot
                                                            (.findActor "cdq.ui.modal-window")))
                                               (on-click))})}]]
               :actor/name "cdq.ui.modal-window"
               :modal? true
               :actor/center-position [(/ (viewport/world-width  ui-viewport) 2)
                                       (* (viewport/world-height ui-viewport) (/ 3 4))]
               :pack? true})))

(defn set-item! [stage cell item-properties]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      (inventory-window/set-item! cell item-properties)))

(defn remove-item! [stage inventory-cell]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      (inventory-window/remove-item! inventory-cell)))

(defn add-skill! [stage skill-properties]
  (-> stage
      .getRoot
      (group/find-actor "cdq.ui.action-bar")
      (action-bar/add-skill! skill-properties)))

(defn remove-skill! [stage skill-id]
  (-> stage
      .getRoot
      (group/find-actor "cdq.ui.action-bar")
      (action-bar/remove-skill! skill-id)))

(defn show-text-message! [stage message]
  (-> stage
      .getRoot
      (group/find-actor "player-message")
      (message/show! message)))

(defn toggle-entity-info-window! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.entity-info")
      toggle-visible!))

(defn close-all-windows! [stage]
  (->> (stage-find stage "cdq.ui.windows")
       group/children
       (run! #(actor/set-visible! % false))))

(defprotocol ActorInformation
  (actor-information [_ actor]))

(defprotocol ErrorWindow
  (show-error-window! [_ throwable]))
