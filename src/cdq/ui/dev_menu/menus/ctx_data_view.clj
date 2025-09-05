(ns cdq.ui.dev-menu.menus.ctx-data-view
  (:require [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.dev.data-view :as data-view]))

(defn items [_ctx _params]
  [{:label "Show data"
    :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                (stage/add! stage (data-view/table-view-window {:title "Context"
                                                                :data ctx
                                                                :width 500
                                                                :height 500})))}])
