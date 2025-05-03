(ns cdq.ui
  (:require [cdq.ui.group :refer [find-actor-with-id add-actor!]]
            [cdq.ui.table :as table])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable Drawable ChangeListener)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align Scaling)
           (com.kotcrab.vis.ui.widget VisTable Tooltip VisImage VisTextButton VisCheckBox VisSelectBox VisImageButton VisTextField VisLabel VisScrollPane VisWindow Separator)
           (cdq StageWithState)))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (Separator. "vertical")
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

(defn- set-center! [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth  actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn- set-actor-opts! [^Actor a {:keys [id name visible? touchable center-position position] :as opts}]
  (when id                          (.setUserObject        a id))
  (when name                        (.setName      a name))
  (when (contains? opts :visible?)  (.setVisible   a (boolean visible?)))
  (when-let [[x y] center-position] (set-center!   a x y))
  (when-let [[x y] position]        (.setPosition  a x y))
  a)

(defn- set-opts [actor opts]
  (set-actor-opts! actor opts)
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

(defn application-state [actor]
  (when-let [stage (Actor/.getStage actor)]
    (.applicationState ^StageWithState stage)))

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

(defn texture-region-drawable ^TextureRegionDrawable
  [^TextureRegion texture-region]
  (TextureRegionDrawable. texture-region))

(defn scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (Actor/.getParent actor)
           (button-class? (Actor/.getParent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (Actor/.getParent actor)]
      (when-let [p (Actor/.getParent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (Actor/.getParent actor)]
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
      ;(println "Clicked on change-listener ... ")
      ;(println "(keys (application-state actor))" (keys (application-state actor)))
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (VisTextButton. (str text))]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (texture-region-drawable texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (BaseDrawable/.setMinSize drawable
                                   (float (* scale w))
                                   (float (* scale h)))))
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
