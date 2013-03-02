<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

    <!-- used for generating nice html from the concordion index xml -->
    <xsl:template match="/concordion-summary">
        <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
            <title>Acceptance Test Reports</title>
            <style type="text/css">
                table {
                    width: 100%;
                    border: black 1px solid;
                }

                td {
                    border: black 1px dotted;
                }

                .sectiontitle {
                    text-align: left;
                    background: lightgray;
                    font-size: 13pt;
                    font-family: arial;
                }

                .sectionheader {
                    background: lightgray;
                }

                .issueheader {
                    width: 10%;
                }

                .successheader {
                    width: 10%;
                }

                .success-false {
                    background-color: red;
                }
            </style>
        </head>
        <body>
            <h1>Acceptance Test Reports</h1>
            <xsl:apply-templates select="section" />
        </body>
        </html>
    </xsl:template>

    <xsl:template match="/concordion-summary/section">
        <table border="1">
            <tr class="sectiontitle">
                <th colspan="3"><xsl:value-of select="@name"/></th>
            </tr>
            <tr class="sectionheader">
                <th class="issueheader">Issue</th>
                <th class="titleheader">Title</th>
                <th class="successheader">Success</th>
            </tr>
            <xsl:apply-templates select="report" />
        </table>
    </xsl:template>

    <xsl:template match="/concordion-summary/section/report">
        <tr>
            <xsl:attribute name="class">
                success-<xsl:value-of select="@success"/>
            </xsl:attribute>
            <td><a href="http://jira.example.com/browse/{@issuenumber}" target="_blank"><xsl:value-of select="@issuenumber"/></a>&#160;</td>
            <td><a href="{@path}"><xsl:value-of select="@title"/></a>&#160;</td>
            <td><xsl:value-of select="@success"/>&#160;</td>
        </tr>
        <xsl:apply-templates select="report" />
    </xsl:template>

</xsl:stylesheet>