(ns cdq.render
  (:require [clojure.data.grid2d :as g2d]))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn- assoc-active-entities [{:keys [cdq.context/content-grid
                                      cdq.context/player-eid]
                               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))

(defn game-loop! [context]
  (reduce (fn [context f]
            (f context))
          context
          (concat [assoc-active-entities]
                  (for [ns-sym '[cdq.render.set-camera-on-player
                                 cdq.render.clear-screen ; application level code
                                 cdq.render.tiled-map
                                 cdq.render.draw-on-world-view
                                 cdq.render.stage
                                 cdq.render.player-state-input
                                 cdq.render.update-mouseover-entity
                                 cdq.render.update-paused
                                 cdq.render.when-not-paused

                                 ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                                 cdq.render.remove-destroyed-entities

                                 cdq.render.camera-controls
                                 cdq.render.window-controls]]
                    (do
                     (require ns-sym)
                     (resolve (symbol (str ns-sym "/render"))))))))
