(ns cdq.ui.dev-menu
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.ui.menu :as menu]
            [cdq.utils :as utils]))

(defn mouseover-entity-id [icon]
  {:label "Mouseover-entity id"
   :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                (when-let [entity (and mouseover-eid @mouseover-eid)]
                  (:entity/id entity)))
   :icon icon})

(defn elapsed-time [icon]
  {:label "elapsed-time"
   :update-fn (fn [ctx]
                (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
   :icon icon})

(def paused
  {:label "paused?"
   :update-fn :ctx/paused?})

(def ui-mouse-position
  {:label "GUI"
   :update-fn (fn [{:keys [ctx/ui-mouse-position]}]
                (mapv int ui-mouse-position))})

(def world-mouse-position
  {:label "World"
   :update-fn (fn [{:keys [ctx/world-mouse-position]}]
                (mapv int world-mouse-position))})

(defn zoom [icon]
  {:label "Zoom"
   :update-fn (comp :camera/zoom :viewport/camera :ctx/world-viewport)
   :icon icon})

(defn fps [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})

(defn- create*
  [{:keys [
           ctx/textures
           ctx/db
           ]}
   {:keys [info]}
   ]
  (menu/create
   {:menus [
            ((requiring-resolve 'cdq.ui.dev-menu.menus.select-world/create))
            ((requiring-resolve 'cdq.ui.dev-menu.menus.help/create) info)
            ((requiring-resolve 'cdq.ui.dev-menu.menus.db/create) db)
            @(requiring-resolve 'cdq.ui.dev-menu.menus.ctx-data-view/item)
            ]
    :update-labels (let [->texture (fn [path] (utils/safe-get textures path))]
                     [
                      (mouseover-entity-id (->texture "images/mouseover.png"))
                      (elapsed-time        (->texture "images/clock.png"))
                      paused
                      ui-mouse-position
                      world-mouse-position
                      (zoom (->texture "images/zoom.png"))
                      (fps (->texture "images/fps.png"))
                      ])}))

(defn create [ctx]
  (create* ctx
           {:info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}))
