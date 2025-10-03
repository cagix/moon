(ns cdq.ctx.create.stage
  (:require [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.message :as message]
            [gdl.graphics.viewport :as viewport]
            [clojure.scene2d.vis-ui.window :as window]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage]
            [com.badlogic.gdx.scenes.scene2d.ui.button :as button]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(defn- add-actors! [stage actor-fns ctx]
  (doseq [[actor-fn & params] actor-fns]
    (stage/add! stage (scene2d/build (apply actor-fn ctx params)))))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   actor-fns]
  (extend-type (class ctx)
    ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (graphics/handle-draws! graphics draws)))
  (let [stage (com.badlogic.gdx.scenes.scene2d.stage/create
               (:graphics/ui-viewport graphics)
               (:graphics/batch       graphics))
        actor-fns (map #(update % 0 requiring-resolve) actor-fns)
        ctx (assoc ctx
                   :ctx/stage stage
                   :ctx/actor-fns actor-fns)]
    (add-actors! stage actor-fns ctx)
    ctx))

(defn- stage-find [stage k]
  (-> stage
      stage/root
      (group/find-actor k)))

(extend-type CtxStage
  ui/DataViewer
  (show-data-viewer! [this data]
    (stage/add! this (scene2d/build
                      {:actor/type :actor.type/data-viewer
                       :title "Data View"
                       :data data
                       :width 500
                       :height 500})))

  ui/Stage
  (viewport-width  [stage] (:viewport/width  (stage/viewport stage)))
  (viewport-height [stage] (:viewport/height (stage/viewport stage)))

  (get-ctx [this]
    (stage/get-ctx this))

  (mouseover-actor [this position]
    (stage/hit this
               (viewport/unproject (stage/viewport this) position)))

  (actor-information [_ actor]
    (let [inventory-slot (inventory-window/cell-with-item? actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window/title-bar? actor) [:mouseover-actor/window-title-bar]
       (button/is?        actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified])))

  (action-bar-selected-skill [this]
    (-> this
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        action-bar/selected-skill))

  (rebuild-actors! [stage ctx]
    (stage/clear! stage)
    (add-actors! stage (:ctx/actor-fns ctx) ctx))

  (inventory-window-visible? [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        actor/visible?))

  (toggle-inventory-visible! [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        actor/toggle-visible!))

  ; no window movable type cursor appears here like in player idle
  ; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
  ; => input events handling
  ; hmmm interesting ... can disable @ item in cursor  / moving / etc.
  (show-modal-window! [stage ui-viewport {:keys [title text button-text on-click]}]
    (assert (not (-> stage
                     stage/root
                     (group/find-actor "cdq.ui.modal-window"))))
    (stage/add! stage
                (scene2d/build
                 {:actor/type :actor.type/window
                  :title title
                  :rows [[{:actor {:actor/type :actor.type/label
                                   :label/text text}}]
                         [{:actor {:actor/type :actor.type/text-button
                                   :text button-text
                                   :on-clicked (fn [_actor _ctx]
                                                 (actor/remove! (-> stage
                                                                    stage/root
                                                                    (group/find-actor "cdq.ui.modal-window")))
                                                 (on-click))}}]]
                  :actor/name "cdq.ui.modal-window"
                  :modal? true
                  :actor/center-position [(/ (:viewport/width  ui-viewport) 2)
                                          (* (:viewport/height ui-viewport) (/ 3 4))]
                  :pack? true})))

  (set-item! [stage cell item-properties]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        (inventory-window/set-item! cell item-properties)))

  (remove-item! [stage inventory-cell]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        (inventory-window/remove-item! inventory-cell)))

  (add-skill! [stage skill-properties]
    (-> stage
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        (action-bar/add-skill! skill-properties)))

  (remove-skill! [stage skill-id]
    (-> stage
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        (action-bar/remove-skill! skill-id)))

  (show-text-message! [stage message]
    (-> stage
        stage/root
        (group/find-actor "player-message")
        (message/show! message)))

  (toggle-entity-info-window! [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.entity-info")
        actor/toggle-visible!))

  (close-all-windows! [stage]
    (->> (stage-find stage "cdq.ui.windows")
         group/children
         (run! #(actor/set-visible! % false))))
  )
