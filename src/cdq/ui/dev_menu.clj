(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.editor]
            [cdq.ui.menu :as menu]))

(defn help-items [_ctx _params]
  [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}])

(defn reset-game-fn [{:keys [ctx/config]} world-fn]
  (swap! application/state (requiring-resolve (:reset-game-state! config)) world-fn))

(defn create
  [{:keys [ctx/textures] :as ctx}]
  (menu/create
   {:menus (map
            (fn [menu]
              (update menu :items (fn [[f params]]
                                    ((requiring-resolve f) ctx params))))
            [
            {:label "World"
             :items ['cdq.ui.dev-menu.menus.select-world/create
                      {:world-fns [['cdq.world-fns.tmx/create
                                    {:tmx-file "maps/vampire.tmx"
                                     :start-position [32 71]}]
                                   ['cdq.world-fns.uf-caves/create
                                    {:tile-size 48
                                     :texture-path "maps/uf_terrain.png"
                                     :spawn-rate 0.02
                                     :scaling 3
                                     :cave-size 200
                                     :cave-style :wide}]
                                   ['cdq.world-fns.modules/create
                                    {:world/map-size 5,
                                     :world/max-area-level 3,
                                     :world/spawn-rate 0.05}]]
                       :reset-game-fn reset-game-fn}
                      ]}

            {:label "Help"
             :items '[cdq.ui.dev-menu/help-items]}

            {:label "Editor"
             :items ['cdq.ui.dev-menu.menus.db/create cdq.editor/open-editor-overview-window!]}

            {:label "Ctx Data"
             :items ['cdq.ui.dev-menu.menus.ctx-data-view/items]}
            ])
    :update-labels (let [->texture (fn [path]
                                     (assert (contains? textures path))
                                     (get textures path))]
                     [((requiring-resolve 'cdq.ui.dev-menu.update-labels.mouseover-entity-id/create) (->texture "images/mouseover.png"))
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.elapsed-time/create)        (->texture "images/clock.png"))
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.paused/item)
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.ui-mouse-position/item)
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.world-mouse-position/item)
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.zoom/create) (->texture "images/zoom.png"))
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.fps/create) (->texture "images/fps.png"))])}))
