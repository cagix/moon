(ns ^:no-doc editor.javafx
  (:require [clojure.java.io :as io]
            [component.db :as db]
            [component.schema :as schema]
            [component.property :as property]
            [dev.javafx :as fx]
            [editor.visui]
            [editor.widget]
            [utils.core :refer [->edn-str safe-get]])
  (:import (javafx.event EventHandler)
           (javafx.scene.control Button CheckBox ComboBox TreeItem TreeView)
           (javafx.scene.image Image ImageView)
           (javafx.scene.layout StackPane)
           (javafx.scene Scene Node)
           (javafx.collections FXCollections))
  #_(:gen-class :extends javafx.application.Application))

(comment
 ; * remove comment at :gen-class
 ; * lein with-profile javafx repl
 ; * lein clean before doing `dev` again (ns refresh doesnt work with aot)
 (db/load! "properties.edn")
 (fx/init)
 (fx/run (properties-tabs))

 )

(defn -start [app stage]
  (def stage stage))

(defmulti schema->widget editor.widget/widget-type)

(import javafx.scene.control.TabPane
        javafx.scene.control.TabPane$TabClosingPolicy
        javafx.scene.control.Tab
        javafx.scene.control.Label
        javafx.scene.control.TextField
        javafx.scene.layout.VBox)

(import javafx.geometry.Insets)
(import javafx.scene.image.ImageView)
(import javafx.scene.image.Image)
(import javafx.scene.layout.FlowPane)
(import javafx.scene.layout.GridPane)

(import javafx.geometry.Rectangle2D)

; TODO
; https://docs.oracle.com/javafx/2/get_started/form.htm
; * new window w. form label, widget, Save, Cancel button
(import javafx.stage.Stage)

(defmethod schema->widget :default [_ v]
  (TextField. (->edn-str v)))

(defmethod schema->widget :boolean [_ v]
  (doto (CheckBox.)
    (.setSelected v)))

(defmethod schema->widget :string [_ v]
  (TextField. v))

#_(defmethod schema->widget :enum [schema v]
  (let [combo-box (ComboBox.)
        items (FXCollections/observableArrayList (map ->edn-str (rest schema)))]
    (.setItems combo-box items)
    combo-box))

(defmethod schema->widget :s/map [_ m]
  (let [grid (doto (GridPane.)
               (.setPadding (Insets. 0 10 0 10))
               (.setVgap 10)
               (.setHgap 10))
        rows (atom -1)]
    (doseq [[k v] (sort-by editor.visui/component-order m)
            :let [row (swap! rows inc)]]
      (.add grid (Label. (str k))                 0 row)
      (.add grid (schema->widget (schema/of k) v) 1 row))
    grid))

(defn- property-editor-window [property-id]
  (let [props (db/get-raw property-id)
        schema (schema/of (property/type props))
        widget (schema->widget schema props)]
    (doto (Stage.)
      (.setTitle (name property-id))
      (.setScene (Scene. widget 450 450))
      .show)))

(require '[component.property :as property])
(require '[component.db :as db])

(def ->image (memoize (fn [file] (Image. file))))

(defn image-view [{:keys [file sub-image-bounds]}]
  (let [[x y w h] sub-image-bounds
        image-view (ImageView. (->image file))]
    (if sub-image-bounds
      (do (.setViewport image-view (Rectangle2D. x y w h))
          image-view)
      image-view)))

(defn- property->button [property]
  (let [image (property/->image property)
        button (if image
                 (Button. "" (image-view (property/->image property)))
                 (Button. (name (:property/id property))))]
    (.setOnAction button (reify EventHandler
                           (handle [_ e]
                             (println (name (:property/id property)))
                             (property-editor-window (:property/id property))
                             )))
    button))

(defn overview-flow-pane [property-type]
  (let [flow (doto (FlowPane.)
               (.setPadding (Insets. 5 0 5 0))
               (.setVgap 4)
               (.setHgap 4)
               (.setPrefWrapLength 500)
               (.setStyle "-f-background-color: DAE6F3;"))
        {:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (property/overview property-type)
        properties (db/all-raw property-type)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    (doseq [property properties]
      (.add (.getChildren flow) (property->button property)))
    flow))

(defn- properties-tabs []
  (let [tab-pane (doto (TabPane.)
                   (.setTabClosingPolicy TabPane$TabClosingPolicy/UNAVAILABLE))]
    (doseq [property-type (sort (property/types))
            :let [tab (Tab. (:title (property/overview property-type))
                            (Label. "whre thsi labl is"))]]
      (.setContent tab (overview-flow-pane property-type))
      (.add (.getTabs tab-pane) tab))
    (let [scene (Scene. (VBox. (into-array Node [tab-pane])))]
      (.setScene stage scene))
    (.setTitle stage "JavaFX App")
    (.show stage)))
