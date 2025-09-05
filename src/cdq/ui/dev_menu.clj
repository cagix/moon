(ns cdq.ui.dev-menu
  (:require [cdq.ui.menu :as menu]))

(defn create
  [{:keys [ctx/textures
           ctx/db]}]
  (menu/create
   {:menus [((requiring-resolve 'cdq.ui.dev-menu.menus.select-world/create))
            ((requiring-resolve 'cdq.ui.dev-menu.menus.help/create) "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
            ((requiring-resolve 'cdq.ui.dev-menu.menus.db/create) db)
            @(requiring-resolve 'cdq.ui.dev-menu.menus.ctx-data-view/item)]
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
