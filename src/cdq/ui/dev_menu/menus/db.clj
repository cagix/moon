(ns cdq.ui.dev-menu.menus.db
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [clojure.string :as str]))

(defn create [{:keys [ctx/db]} _]
  (for [property-type (sort (db/property-types db))]
    {:label (str/capitalize (name property-type))
     :on-click (fn [_actor ctx]
                 (ctx/handle-txs! ctx [[:tx/open-editor-overview property-type]]))}))
