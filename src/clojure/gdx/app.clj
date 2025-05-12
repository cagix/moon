(ns clojure.gdx.app)

(defprotocol Application
  (post-runnable!* [_ runnable]
                  "Posts a `java.lang.Runnable` on the main loop thread."))

(defmacro post-runnable!
  "Posts expressions as a lambda Runnable on the main loop thread."
  [app & exprs]
  `(post-runnable!* app (fn [] ~@exprs)))
