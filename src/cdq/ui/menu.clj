(ns cdq.ui.menu
  (:import (clojure.lang MultiFn)))

(defn init! [ctx]
  (MultiFn/.addMethod @(requiring-resolve 'clojure.scene2d/build)
                      :actor.type/menu-bar
                      (requiring-resolve 'clojure.gdx.scene2d.actor.menu-bar/create))
  ctx)
