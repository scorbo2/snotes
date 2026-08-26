package ca.corbett.snotes.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.mcp.McpServer;
import ca.corbett.snotes.AppConfig;
import ca.corbett.snotes.Version;
import ca.corbett.snotes.extensions.SnotesExtension;
import ca.corbett.snotes.service.NoteService;
import ca.corbett.snotes.ui.MainWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * This built-in extension stands up an MCP server, and exposes functionality from our
 * service layer as MCP tools that can be invoked by an LLM agent or other MCP client.
 * We currently expose tools to create, save, and search for notes. Template and Query CRUD
 * is not yet exposed, but may be in a future release.
 * <p>
 *     Security note: this extension is intended to run locally in a single-user environment.
 *     We therefore bind the MCP server to all interfaces (0.0.0.0). The mcp-light server
 *     runs without authentication, so anyone on the LAN can therefore create and search
 *     notes. If security is a concern, lock the exposed port behind a firewall, or
 *     disable this extension entirely. (The server does not run when the extension is disabled.)
 *     Note that you can disable/enable the extension without restarting the application,
 *     by visiting the ExtensionManager dialog.
 * </p>
 * <p>
 *     Performance note: beware of large searches via the search tool! There is no cap
 *     enforced in the code for returned results, so a large search can VERY EASILY fill
 *     your entire context window.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public class McpExtension extends SnotesExtension {

    private static final Logger log = Logger.getLogger(McpExtension.class.getName());
    private static final int DEFAULT_PORT = 5000;
    private final AppExtensionInfo extInfo;
    private McpServer mcpServer;

    public McpExtension() {
        extInfo = new AppExtensionInfo.Builder("McpExtension")
            .setTargetAppName(Version.NAME)
            .setVersion(Version.VERSION)
            .setTargetAppVersion(Version.VERSION)
            .setAuthor("Steve Corbett")
            .setAuthorUrl("https://github.com/scorbo2")
            .setShortDescription("Exposes Snotes functionality as MCP tools for LLM agents and other MCP clients.")
            .setLongDescription("This built-in extension stands up an MCP server, and exposes functionality "
                                + "from our service layer as MCP tools that can be invoked by an LLM agent or other MCP client. "
                                + "We currently expose tools to create, save, and search for notes. "
                                + "Template and Query CRUD is not yet exposed, but may be in a future release.")
            .build();
        mcpServer = null;
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void onActivate() {
        int port = getConfiguredPort();
        if (mcpServer == null) {
            try {
                mcpServer = new McpServer(port);
                mcpServer.registerTool(new McpCreateNoteTool(noteServiceSupplier()));
                mcpServer.registerTool(new McpSearchNotesTool(noteServiceSupplier()));
                mcpServer.start();
                log.info("MCP server started on port " + port);
            }
            catch (Exception e) {
                log.severe("Failed to start MCP server: " + e.getMessage());
                mcpServer = null;
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (mcpServer != null) {
            mcpServer.stop();
            mcpServer = null;
        }
    }

    /**
     * Returns a lazy supplier for the application's NoteService, to be injected into
     * our MCP tools.
     * <p>
     *     Don't try to retrieve the service directly in onActivate()!!!
     *     MainWindow is lazily loaded only AFTER all extensions have been loaded and activated.
     *     Trying to get it in onActivate() will have very bad results. The tools evaluate
     *     this supplier only when they are actually invoked, by which time MainWindow is
     *     guaranteed to exist.
     * </p>
     */
    private static Supplier<NoteService> noteServiceSupplier() {
        return () -> MainWindow.getInstance().getNoteService();
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(new IntegerProperty("MCP.server.port", "MCP Server Port", DEFAULT_PORT, 1025, 65535, 1));
        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here.
    }

    /**
     * Invoked internally to look up and return our configured port number.
     * Note: we use AppConfig.peek() here because of load ordering issues.
     * Our onActivate() gets invoked BEFORE the config file is loaded.
     */
    private int getConfiguredPort() {
        String rawValue = AppConfig.peek("MCP.server.port");
        try {
            int port = Integer.parseInt(rawValue);

            // Note we need to do our own range checking here, because we can't
            // get access to the IntegerProperty yet. We only get the raw value as a string,
            // and it could technically be anything if the user hand-edited the config file.
            return (port >= 1025 && port <= 65535) ? port : DEFAULT_PORT;
        }
        catch (NumberFormatException nfe) {
            return DEFAULT_PORT;
        }
    }
}
