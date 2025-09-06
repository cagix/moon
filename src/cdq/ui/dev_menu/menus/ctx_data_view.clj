(ns cdq.ui.dev-menu.menus.ctx-data-view
  (:require [cdq.ui.data-viewer :as data-viewer]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn items [_ctx _params]
  [{:label "Show data"
    :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                (stage/add! stage (data-viewer/create
                                   {:title "Context"
                                    :data ctx
                                    :width 500
                                    :height 500})))}])
