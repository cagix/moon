(ns clojure.ctx.editor
  (:require [clojure.db :as db]
            [clojure.ui :as ui]
            [clojure.ui.editor]
            [clojure.ui.editor.overview-table]
            [clojure.ui.stage :as stage]))

(defn- open-property-editor-window! [id {:keys [ctx/db
                                                ctx/stage]
                                         :as ctx}]
  (stage/add! stage (clojure.ui.editor/editor-window (db/get-raw db id) ctx)))

(defn open-editor-overview-window! [{:keys [ctx/stage] :as ctx} property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (clojure.ui.editor.overview-table/create ctx
                                                             property-type
                                                             open-property-editor-window!))
    (.pack window)
    (stage/add! stage window)))
