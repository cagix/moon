(ns gdl.ui
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.input :as input]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :as group :refer [find-actor-with-id add-actor!]]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [gdl.scene2d.ui.utils :as scene2d.utils]
            [gdl.utils :as utils]
            [clojure.gdx.vis-ui.widgets.separator :as separator])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Widget Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window Tree$Node)
           (com.badlogic.gdx.scenes.scene2d.utils Drawable ChangeListener ClickListener)
           (com.badlogic.gdx.utils Align Scaling)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget VisTable Tooltip Menu MenuBar MenuItem VisImage VisTextButton VisCheckBox VisSelectBox VisImageButton VisTextField VisLabel VisScrollPane VisTree VisWindow)
           (gdl StageWithState)))

(defn horizontal-separator-cell [colspan]
  {:actor (separator/horizontal)
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (separator/vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-opts [actor opts]
  (actor/set-opts actor opts)
  (when (instance? Table actor)
    (table/set-opts actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(add-actor! group %) actors)
    (set-opts group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad]}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    group))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(add-actor! group %) actors)
    group))

(defn- create-stage [viewport batch actors]
  (let [stage (proxy [StageWithState clojure.lang.ILookup] [viewport batch]
                (valAt
                  ([id]
                   (group/find-actor-with-id (StageWithState/.getRoot this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (StageWithState/.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    stage))

(defn application-state [actor]
  (when-let [stage (Actor/.getStage actor)]
    (.applicationState ^StageWithState stage)))

(defn draw [^StageWithState stage context]
  (set! (.applicationState stage) context)
  (stage/draw stage))

(defn act [^StageWithState stage context]
  (set! (.applicationState stage) context)
  (stage/act stage))

(defn add-tooltip!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (when-let [context (application-state (.getTarget this))]
                          (.setText this (str (tooltip-text context)))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [^Actor actor]
  (Tooltip/removeTooltip actor))

(defn button-group [{:keys [max-check-count min-check-count]}]
  (let [bg (ButtonGroup.)]
    (.setMaxCheckCount bg max-check-count)
    (.setMinCheckCount bg min-check-count)
    bg))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(def checked? VisCheckBox/.isChecked)

(defn select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(def selected VisSelectBox/.getSelected)

(defn table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts opts)))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts opts)))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn text-field [text opts]
  (-> (VisTextField. (str text))
      (set-opts opts)))

(def text-field->text VisTextField/.getText)

(defn ui-stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [^Texture texture]
  (VisImage. (TextureRegion. texture)))

(defmethod image* TextureRegion [^TextureRegion tr]
  (VisImage. tr))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(def texture-region-drawable scene2d.utils/texture-region-drawable)

(defn scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (Actor/.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (actor/parent actor)
           (button-class? (actor/parent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (actor/parent actor)]
      (when-let [p (actor/parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (actor/parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [^Actor actor]
  (.pack (find-ancestor-window actor)))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (VisTextButton. (str text))]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (texture-region-drawable texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] (texture-region/dimensions texture-region)]
         (scene2d.utils/set-min-size! drawable (* scale w) (* scale h))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn ui-actor ^Actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw
        (draw (application-state this))))
    (act [_delta]
      (if (Actor/.getStage this)
        (let [context (application-state this)]
          (assert context)
          (assert (:gdl/input context)
                  (str "(pr-str (sort (keys context))): " (pr-str (sort (keys context)))))
          (when act
            (act (application-state this))))
        (do
         ; called x1 time when editor window opened then closed.
         ; is removed during stage actors iteration
         ; so then is called
         ; ... ... so no context !
         ; for draw not possible...
         ; so my assumption was:
         ; * an actor always has a stage....
         ; so it should be removed from iterating ...
         ; or the actor should get the context from stage itself not in act ....
         ; act should pass the context ! that's where its really coming from ...
         ; so thread it through with stage ... libgdx
         (println "actor act called but not part of stage."))))))

(defn ui-widget [draw!]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (draw! this (application-state this)))))

(defn set-drawable! [^Image image drawable]
  (.setDrawable image drawable))

(defn t-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn menu [label]
  (Menu. label))

(defn menu-bar []
  (MenuBar.))

(def menu-bar->table MenuBar/.getTable)
(def add-menu        MenuBar/.addMenu)

(defn menu-item ^MenuItem [text]
  (MenuItem. text))

(defn window? [actor]
  (instance? VisWindow actor))

(defn tree []
  (VisTree.))

(defn click-listener
  "Detects mouse over, mouse or finger touch presses, and clicks on an actor. A touch must go down over the actor and is considered pressed as long as it is over the actor or within the tap square. This behavior makes it easier to press buttons on a touch interface when the initial touch happens near the edge of the actor. Double clicks can be detected using getTapCount(). Any touch (not just the first) will trigger this listener. While pressed, other touch downs are ignored."
  [clicked-fn]
  (proxy [ClickListener] []
    (clicked [event x y]
      (clicked-fn {:event event :x x :y y}))))

(defn setup-stage! [{:keys [gdl/config
                            gdl.graphics/batch
                            gdl.graphics/ui-viewport
                            gdl/input]
                     :as context}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case (::skin-scale config)
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (let [actors (map (fn [create]
                      ((utils/require-ns-resolve create) context))
                    (::actors config))
        stage (create-stage ui-viewport batch actors)]
    (input/set-processor input stage)
    (assoc context :gdl.context/stage stage)))

(defn dispose! []
  (VisUI/dispose))
