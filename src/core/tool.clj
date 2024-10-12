(ns core.tool
  (:require [clojure.java.io :as io])
  (:import (javafx.event EventHandler)
           (javafx.scene.control Button TreeItem TreeView)
           (javafx.scene.image Image ImageView)
           (javafx.scene.layout StackPane)
           (javafx.scene Scene Node))
  (:gen-class
   :extends javafx.application.Application))

; https://stackoverflow.com/questions/66978726/clojure-javafx-live-manipulation
; https://github.com/dlsc-software-consulting-gmbh/FormsFX
; https://github.com/antoniopelusi/JavaFX-Dark-Theme?tab=readme-ov-file

(defn- clj-file-forms [file]
  (read-string (str "[" (slurp file) "]")))

(require '[rewrite-clj.parser :as p])

(comment
 (p/parse-file-all "src/core/stat.clj")

 (p/parse-file-all "../clojure.gdx/src/clojure/gdx/java.clj")


 )

(comment
 (first (doall (map (partial take 2) (clj-file-forms "src/clojure/gdx.clj")))))

(declare stage)

(defn- first-sym->icon [sym]
  (case (str sym)
    "ns" "📦"
    "def" "📌"
    "declare" "🔖"
    "defmacro" "🏗️"
    "defn" "📜"
    "defn-" "🔧"
    "defmulti" "🛠️"
    "defmethod" "🧩"
    "defsystem" "🔄"
    "defc" "🔑"
    "comment" "📖"
    "derive" "🧬"
    "defprotocol" "📄"
    "defrecord" "🗃️"
    "?"))

(defn- syms->text [[first-sym second-sym]]
  (str (first-sym->icon first-sym) " " first-sym " " second-sym))

(import javafx.scene.layout.Background)
(import javafx.scene.layout.BackgroundImage)
(import javafx.scene.layout.BackgroundRepeat)
(import javafx.scene.layout.BackgroundPosition)
(import javafx.scene.layout.BackgroundSize)

(defn clj-file-forms-tree [file]
  (.setTitle stage "clj-file-forms-tree")
  (let [clj-forms (doall (map (partial take 2) (clj-file-forms file)))
        ;root-icon (ImageView. (Image. (io/input-stream (io/resource "images/animations/vampire-1.png"))))
        root-item (TreeItem. (syms->text (first clj-forms)), #_root-icon)]
    (.setExpanded root-item true)
    (doseq [syms (rest clj-forms)
            ;file (map str (file-seq (io/file "src/")))
            ]
      (.add (.getChildren root-item) (TreeItem. (syms->text syms)
                                                ;file
                                                )))
    (let [stack-pane (StackPane.)]
      (.add (.getChildren stack-pane) (TreeView. root-item))
      (let [scene (Scene. stack-pane 400 900)]
        (.add (.getStylesheets scene) (.toExternalForm (io/resource "darkmode.css")))
        (.setScene stage scene))
      (.show stage))))

(comment
 (fx-run (clj-file-forms-tree "src/clojure/gdx.clj"))

 )

(import javafx.scene.control.TabPane
        javafx.scene.control.TabPane$TabClosingPolicy
        javafx.scene.control.Tab
        javafx.scene.control.Label
        javafx.scene.layout.VBox)

(import javafx.geometry.Insets)
(import javafx.scene.image.ImageView)
(import javafx.scene.image.Image)
(import javafx.scene.layout.FlowPane)

(defmacro fx-run
  "With this macro what you run is run in the JavaFX Application thread.
  Is needed for all calls related with JavaFx"
  [& code]
  `(javafx.application.Platform/runLater (fn [] ~@code)))

(import javafx.geometry.Rectangle2D)

; TODO
; https://docs.oracle.com/javafx/2/get_started/form.htm
; * new window w. form label, widget, Save, Cancel button
(import javafx.stage.Stage)

(comment
 (fx-run
  (let [scene (Scene. (FlowPane.) 450 450)
        stage (doto (Stage.)
                (.setTitle "FOOBAR")
                (.setScene scene)
                .show)]))
 )

#_(comment
 (prop->image (first (all-properties-raw :properties/audiovisuals)))
 {:file "images/oryx_16bit_scifi_FX_lg_trans.png", :sub-image-bounds [64 64 32 32]}

 (def ->image-view (memoize (fn [file] (ImageView. (Image. file)))))

 (defn- ->image-view [{:keys [file sub-image-bounds]}]
   (let [imgview (->image-view file)]
     (if sub-image-bounds
       imgview
       )
     )
   (doto imgview
     (.setViewport (Rectangle2D. (* x size) (* y size) size size)))
   ))

#_(defn overview-flow-pane [property-type]
  (let [flow (doto (FlowPane.)
               (.setPadding (Insets. 5 0 5 0))
               (.setVgap 4)
               (.setHgap 4)
               (.setPrefWrapLength 500)
               (.setStyle "-f-background-color: DAE6F3;"))
        {:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (all-properties-raw property-type)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    (doseq [property properties]
      (.add (.getChildren flow) #_(Button. (name (:property/id property)))
            (doto (Button. (:property/id property))
              (.setGraphic (doto (ImageView. image)
                             (.setViewport (Rectangle2D. (* x size) (* y size) size size)))))))
    flow))

(declare my-stage)

(require '[clojure.gdx :refer :all])

(comment
 (fx-run
  (let [tab-pane (doto (TabPane.)
                   (.setTabClosingPolicy TabPane$TabClosingPolicy/UNAVAILABLE))]
    (doseq [property-type (sort (types))
            :let [tab (Tab. (:title (overview property-type)) (Label. "whre thsi labl is"))]]
      (.setContent tab (overview-flow-pane property-type))
      (.add (.getTabs tab-pane) tab))
    (let [scene (Scene. (VBox. (into-array Node [tab-pane])))]
      (.setScene my-stage scene))
    (.setTitle my-stage "JavaFX App")
    (.show my-stage)))

 )

(defn -start [app the-stage]
  (def stage the-stage))

(defn- start-stuff []
  (javafx.application.Platform/setImplicitExit false)
  (.start (Thread. #(javafx.application.Application/launch core.tool (into-array String [""])))) )

(defn -main [& args]
  (start-stuff))
