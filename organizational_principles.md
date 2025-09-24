# Organize code by functions, not data

See `cdq.application.create.info` - before it was in x number of namespaces which all required `clojure.string` and `clojure.utils/readable-number` etc.

# Starting point gave me the idea:

`resources/cdq.game.edn`:

```
{:title "Cyber Dungeon Quest"
 :windowed-mode {:width 1440
                 :height 900}
 :foreground-fps 60
 :listener {:create  cdq.application.create/do!
            :dispose cdq.application.dispose/do!
            :pause   cdq.application.pause/do!
            :render  cdq.application.render/do!
            :resize  cdq.application.resize/do!
            :resume  cdq.application.resume/do!}
 :mac {:glfw-async? true
       :taskbar-icon "icon.png"}}
```

# Make small files!

# Keep monorepo ! You are making a game, not an engine
